import {
  collection, getDocs, doc, updateDoc, deleteDoc
} from "https://www.gstatic.com/firebasejs/13.2.0/firebase-firestore.js";
import { db } from "./firebase-config.js";

const tbody = document.querySelector("#users-table tbody");
const searchInput = document.getElementById("users-search");

let allUsers = [];

export async function initUsersTab() {
  const snap = await getDocs(collection(db, "Users"));
  allUsers = snap.docs.map((d) => ({ uid: d.id, ...d.data() }));
  render(allUsers);
}

searchInput.addEventListener("input", () => {
  const q = searchInput.value.trim().toLowerCase();
  const filtered = allUsers.filter((u) =>
    (u.name || "").toLowerCase().includes(q) ||
    (u.studentID || "").toLowerCase().includes(q) ||
    (u.email || "").toLowerCase().includes(q)
  );
  render(filtered);
});

function render(users) {
  tbody.innerHTML = "";

  if (users.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty">회원이 없습니다</td></tr>`;
    return;
  }

  for (const u of users) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><input type="text" class="edit-name" value="${escapeAttr(u.name)}" /></td>
      <td><input type="text" class="edit-studentID" value="${escapeAttr(u.studentID)}" /></td>
      <td><input type="text" class="edit-department" value="${escapeAttr(u.department)}" /></td>
      <td><input type="text" class="edit-email" value="${escapeAttr(u.email)}" /></td>
      <td>
        <input type="checkbox" class="admin-toggle" ${u.isAdmin ? "checked" : ""} />
      </td>
      <td>
        <button class="btn-small save-user">저장</button>
        <button class="btn-small btn-danger delete-user">삭제</button>
      </td>
    `;

    tr.querySelector(".admin-toggle").addEventListener("change", async (e) => {
      await updateDoc(doc(db, "Users", u.uid), { isAdmin: e.target.checked });
      u.isAdmin = e.target.checked;
    });

    tr.querySelector(".save-user").addEventListener("click", async () => {
      const updated = {
        name: tr.querySelector(".edit-name").value.trim(),
        studentID: tr.querySelector(".edit-studentID").value.trim(),
        department: tr.querySelector(".edit-department").value.trim(),
        email: tr.querySelector(".edit-email").value.trim()
      };
      await updateDoc(doc(db, "Users", u.uid), updated);
      Object.assign(u, updated);
      alert("저장되었습니다.");
    });

    tr.querySelector(".delete-user").addEventListener("click", async () => {
      if (!confirm(`${u.name || u.uid} 회원 정보를 삭제할까요?\n(Firebase Auth 계정 자체는 삭제되지 않습니다)`)) return;
      await deleteDoc(doc(db, "Users", u.uid));
      allUsers = allUsers.filter((x) => x.uid !== u.uid);
      render(allUsers);
    });

    tbody.appendChild(tr);
  }
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));
}

function escapeAttr(str) {
  return escapeHtml(str ?? "");
}
