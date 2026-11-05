import {
  collection, getDocs, doc, updateDoc, deleteDoc, query, orderBy
} from "https://www.gstatic.com/firebasejs/13.2.0/firebase-firestore.js";
import { db } from "./firebase-config.js";

const CONFIGS = {
  studyroom: {
    path: ["reservations", "studyroom", "studyroom"],
    tbody: document.querySelector("#studyroom-table tbody"),
    columns: ["date", "time", "room", "seat", "userUid", "active"]
  },
  meetingroom: {
    path: ["reservations", "meetingroom", "meetingroom"],
    tbody: document.querySelector("#meetingroom-table tbody"),
    columns: ["date", "time", "endTime", "room", "userUid", "active"]
  }
};

export async function initReservationsTab() {
  await Promise.all([renderReservations("studyroom"), renderReservations("meetingroom")]);
}

async function renderReservations(kind) {
  const cfg = CONFIGS[kind];
  let snap;
  try {
    snap = await getDocs(query(collection(db, ...cfg.path), orderBy("date")));
  } catch {
    snap = await getDocs(collection(db, ...cfg.path));
  }

  cfg.tbody.innerHTML = "";

  if (snap.empty) {
    cfg.tbody.innerHTML = `<tr><td colspan="${cfg.columns.length + 1}" class="empty">예약 내역이 없습니다</td></tr>`;
    return;
  }

  for (const d of snap.docs) {
    const data = d.data();
    const tr = document.createElement("tr");

    const cells = cfg.columns.map((col) => {
      if (col === "active") return `<td>${data.active ? "예약중" : "만료/취소"}</td>`;
      return `<td>${escapeHtml(data[col] ?? "-")}</td>`;
    }).join("");

    tr.innerHTML = cells + `
      <td>
        <button class="btn-small cancel-res" ${data.active ? "" : "disabled"}>취소</button>
        <button class="btn-small btn-danger delete-res">삭제</button>
      </td>
    `;

    tr.querySelector(".cancel-res").addEventListener("click", async () => {
      await updateDoc(doc(db, ...cfg.path, d.id), { active: false });
      await renderReservations(kind);
    });

    tr.querySelector(".delete-res").addEventListener("click", async () => {
      if (!confirm("이 예약을 완전히 삭제할까요?")) return;
      await deleteDoc(doc(db, ...cfg.path, d.id));
      await renderReservations(kind);
    });

    cfg.tbody.appendChild(tr);
  }
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));
}
