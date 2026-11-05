import {
  collection, collectionGroup, getDocs, getDoc, doc, addDoc, setDoc, updateDoc, deleteDoc,
  serverTimestamp, Timestamp, orderBy, query, where
} from "https://www.gstatic.com/firebasejs/13.2.0/firebase-firestore.js";
import { db } from "./firebase-config.js";

const courseListEl = document.getElementById("course-list");
const courseForm = document.getElementById("course-form");
const courseDetail = document.getElementById("course-detail");
const courseDetailTitle = document.getElementById("course-detail-title");

let courses = [];
let selectedCourseId = null;

// 과제/학습자료의 courseName은 홈/캘린더 화면이 상위 Courses 문서를 다시 조회하지 않고
// 과제 문서 자체에서 바로 읽기 때문에(denormalized) 반드시 함께 저장해야 함.
const SUB_SCHEMAS = {
  notices: {
    label: "공지사항",
    fields: [
      { key: "title", label: "제목", type: "text" },
      { key: "author", label: "작성자", type: "text" },
      { key: "content", label: "내용", type: "textarea" }
    ],
    orderField: "createdAt"
  },
  materials: {
    label: "학습자료",
    fields: [
      { key: "title", label: "제목", type: "text" },
      { key: "content", label: "내용", type: "textarea" },
      { key: "fileName", label: "첨부파일명", type: "text" },
      { key: "fileUrl", label: "첨부파일 URL (gs:// 또는 https://)", type: "text" }
    ],
    orderField: "createdAt"
  },
  assignments: {
    label: "과제",
    fields: [
      { key: "title", label: "제목", type: "text" },
      { key: "description", label: "설명", type: "textarea" },
      { key: "startAt", label: "시작일", type: "date" },
      { key: "endAt", label: "마감일", type: "date" },
      { key: "scoreTotal", label: "배점", type: "number" },
      { key: "fileName", label: "첨부파일명", type: "text" },
      { key: "fileUrl", label: "첨부파일 URL (gs:// 또는 https://)", type: "text" }
    ],
    orderField: "startAt"
  },
  classes: {
    label: "온라인 강의",
    fields: [
      { key: "title", label: "제목", type: "text" },
      { key: "weekNumber", label: "주차", type: "number" },
      { key: "durationSeconds", label: "재생시간(초)", type: "number" },
      { key: "videoUrl", label: "영상 URL (gs:// 또는 https://)", type: "text" },
      { key: "startAt", label: "수강 시작일", type: "date" },
      { key: "endAt", label: "수강 마감일", type: "date" }
    ],
    orderField: "weekNumber"
  }
};

// files 배열로 합쳐져야 하는 필드 (fileName/fileUrl → files: [{fileName, fileUrl}])
const FILE_FIELD_SUBS = new Set(["materials", "assignments"]);

export async function initCoursesTab() {
  await loadCourseList();
}

async function loadCourseList() {
  const snap = await getDocs(collection(db, "Courses"));
  courses = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
  renderCourseList();
}

function renderCourseList() {
  courseListEl.innerHTML = "";

  if (courses.length === 0) {
    courseListEl.innerHTML = `<li class="empty">등록된 강의가 없습니다</li>`;
    return;
  }

  for (const c of courses) {
    const li = document.createElement("li");
    li.className = "course-item" + (c.id === selectedCourseId ? " selected" : "");
    li.innerHTML = `
      <span class="course-item-name">${escapeHtml(c.courseName || "(이름 없음)")} <small>${escapeHtml(c.year || "")}-${escapeHtml(c.semester || "")} ${escapeHtml(c.section || "")}반</small></span>
      <button class="btn-small delete-course">삭제</button>
    `;
    li.querySelector(".course-item-name").addEventListener("click", () => selectCourse(c.id));
    li.querySelector(".delete-course").addEventListener("click", async (e) => {
      e.stopPropagation();
      if (!confirm(`'${c.courseName}' 강의를 삭제할까요? (하위 공지/자료/과제는 남아있을 수 있습니다)`)) return;
      await deleteDoc(doc(db, "Courses", c.id));
      if (selectedCourseId === c.id) {
        selectedCourseId = null;
        courseDetail.classList.add("hidden");
      }
      await loadCourseList();
    });
    courseListEl.appendChild(li);
  }
}

courseForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const courseName = document.getElementById("course-name").value.trim();
  const courseCode = document.getElementById("course-code").value.trim();
  const professor = document.getElementById("course-professor").value.trim();
  const section = document.getElementById("course-section").value.trim();
  const room = document.getElementById("course-room").value.trim();
  const year = document.getElementById("course-year").value.trim();
  const semester = document.getElementById("course-semester").value.trim();
  const credit = document.getElementById("course-credit").value.trim();

  if (!courseName || !courseCode || !year || !semester || !section) {
    alert("강의명 / 학수번호 / 연도 / 학기 / 분반은 필수입니다.");
    return;
  }

  // 기존 데이터 명명 규칙과 동일하게 맞춤: {year}_{semester}_{courseCode}_{section}
  const courseId = `${year}_${semester}_${courseCode}_${section}`;

  await setDoc(doc(db, "Courses", courseId), {
    courseName, courseCode, professor, section, room, year, semester, credit
  });
  courseForm.reset();
  await loadCourseList();
});

async function selectCourse(courseId) {
  selectedCourseId = courseId;
  renderCourseList();

  const course = courses.find((c) => c.id === courseId);
  courseDetailTitle.textContent = `${course.courseName || ""} 관리`;
  courseDetail.classList.remove("hidden");

  await renderEnrollmentSection(course);

  for (const subName of Object.keys(SUB_SCHEMAS)) {
    await renderSubcollectionSection(course, subName);
  }
}

async function renderEnrollmentSection(course) {
  const sectionEl = document.getElementById("sub-enrollment");
  sectionEl.innerHTML = `
    <h4>수강생 관리</h4>
    <form class="sub-form" id="enroll-form">
      <label>학번으로 추가
        <input type="text" id="enroll-studentId" placeholder="학번 입력" />
      </label>
      <div class="sub-form-actions">
        <button type="submit">수강생 추가</button>
      </div>
    </form>
    <p class="error" id="enroll-error"></p>
    <div class="sub-list" id="enroll-list"></div>
  `;

  const form = sectionEl.querySelector("#enroll-form");
  const errorEl = sectionEl.querySelector("#enroll-error");
  const listEl = sectionEl.querySelector("#enroll-list");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorEl.textContent = "";

    const studentId = sectionEl.querySelector("#enroll-studentId").value.trim();
    if (!studentId) return;

    const userSnap = await getDocs(query(collection(db, "Users"), where("studentID", "==", studentId)));
    if (userSnap.empty) {
      errorEl.textContent = `학번 '${studentId}'인 회원을 찾을 수 없습니다.`;
      return;
    }

    const userDoc = userSnap.docs[0];
    const uid = userDoc.id;

    // 이미 수강 중인 과목이면(과거에 생성된 문서 포함) 그 문서를 재활성화, 아니면 새로 생성
    const existingSnap = await getDocs(
      query(collection(db, "Users", uid, "myCourses"), where("courseId", "==", course.id))
    );

    if (!existingSnap.empty) {
      await updateDoc(doc(db, "Users", uid, "myCourses", existingSnap.docs[0].id), { isActive: true });
    } else {
      await addDoc(collection(db, "Users", uid, "myCourses"), { courseId: course.id, isActive: true });
    }

    form.reset();
    await loadEnrolledStudents(course, listEl);
  });

  await loadEnrolledStudents(course, listEl);
}

async function loadEnrolledStudents(course, listEl) {
  listEl.innerHTML = `<p class="empty">불러오는 중...</p>`;

  let enrolledSnap;
  try {
    enrolledSnap = await getDocs(
      query(collectionGroup(db, "myCourses"), where("courseId", "==", course.id), where("isActive", "==", true))
    );
  } catch (err) {
    console.error("수강생 목록 조회 실패:", err);
    listEl.innerHTML = `<p class="error">수강생 목록을 불러오지 못했습니다: ${escapeHtml(err.message)}</p>`;
    return;
  }

  if (enrolledSnap.empty) {
    listEl.innerHTML = `<p class="empty">수강 중인 학생이 없습니다</p>`;
    return;
  }

  const rows = await Promise.all(enrolledSnap.docs.map(async (enrollDoc) => {
    const uid = enrollDoc.ref.parent.parent.id;
    const userSnap = await getDoc(doc(db, "Users", uid));
    const user = userSnap.exists() ? userSnap.data() : {};
    return { uid, enrollDocId: enrollDoc.id, name: user.name, studentID: user.studentID };
  }));

  listEl.innerHTML = "";
  for (const r of rows) {
    const row = document.createElement("div");
    row.className = "sub-item";
    row.innerHTML = `
      <span>${escapeHtml(r.studentID || r.uid)} ${r.name ? "— " + escapeHtml(r.name) : ""}</span>
      <span class="sub-item-actions">
        <button class="btn-small btn-danger remove-enroll">제외</button>
      </span>
    `;
    row.querySelector(".remove-enroll").addEventListener("click", async () => {
      if (!confirm(`${r.name || r.studentID || r.uid} 학생을 이 강의에서 제외할까요?`)) return;
      await updateDoc(doc(db, "Users", r.uid, "myCourses", r.enrollDocId), { isActive: false });
      await loadEnrolledStudents(course, listEl);
    });
    listEl.appendChild(row);
  }
}

async function renderSubcollectionSection(course, subName) {
  const schema = SUB_SCHEMAS[subName];
  const sectionEl = document.getElementById(`sub-${subName}`);
  sectionEl.innerHTML = `<h4>${schema.label}</h4>`;

  const listEl = document.createElement("div");
  listEl.className = "sub-list";
  sectionEl.appendChild(listEl);

  const formEl = buildSubForm(course, subName, schema, listEl);
  sectionEl.appendChild(formEl);

  await loadSubItems(course.id, subName, schema, listEl, formEl);
}

async function loadSubItems(courseId, subName, schema, listEl, formEl) {
  let docsSnap;
  try {
    docsSnap = await getDocs(query(collection(db, "Courses", courseId, subName), orderBy(schema.orderField)));
  } catch {
    docsSnap = await getDocs(collection(db, "Courses", courseId, subName));
  }

  listEl.innerHTML = "";

  if (docsSnap.empty) {
    listEl.innerHTML = `<p class="empty">등록된 항목이 없습니다</p>`;
    return;
  }

  for (const d of docsSnap.docs) {
    const data = d.data();
    const row = document.createElement("div");
    row.className = "sub-item";
    row.innerHTML = `
      <span>${escapeHtml(summarize(schema, data))}</span>
      <span class="sub-item-actions">
        <button class="btn-small edit-sub">수정</button>
        <button class="btn-small btn-danger delete-sub">삭제</button>
      </span>
    `;

    row.querySelector(".edit-sub").addEventListener("click", () => fillForm(formEl, schema, d.id, data));
    row.querySelector(".delete-sub").addEventListener("click", async () => {
      if (!confirm("이 항목을 삭제할까요?")) return;
      await deleteDoc(doc(db, "Courses", courseId, subName, d.id));
      await loadSubItems(courseId, subName, schema, listEl, formEl);
    });

    listEl.appendChild(row);
  }
}

function summarize(schema, data) {
  const main = data[schema.fields[0].key] || "(제목 없음)";
  const rest = schema.fields.slice(1, 3)
    .map((f) => data[f.key])
    .filter((v) => v !== undefined && v !== "")
    .join(" | ");
  return rest ? `${main} — ${rest}` : main;
}

function buildSubForm(course, subName, schema, listEl) {
  const courseId = course.id;
  const form = document.createElement("form");
  form.className = "sub-form";
  form.dataset.editingId = "";

  form.innerHTML = schema.fields.map((f) => `
    <label>
      ${f.label}
      ${f.type === "textarea"
        ? `<textarea name="${f.key}"></textarea>`
        : `<input type="${f.type}" name="${f.key}" />`}
    </label>
  `).join("") + `
    <div class="sub-form-actions">
      <button type="submit">저장</button>
      <button type="button" class="btn-cancel-edit hidden">취소</button>
    </div>
  `;

  const cancelBtn = form.querySelector(".btn-cancel-edit");
  cancelBtn.addEventListener("click", () => resetForm(form, cancelBtn));

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {};
    for (const f of schema.fields) {
      const raw = form.elements[f.key].value;
      if (f.type === "number") payload[f.key] = raw === "" ? 0 : Number(raw);
      else if (f.type === "date") payload[f.key] = raw ? Timestamp.fromDate(new Date(raw)) : null;
      else payload[f.key] = raw;
    }

    // fileName/fileUrl → files 배열로 합치기 (앱이 files[].fileName / files[].fileUrl만 읽음)
    if (FILE_FIELD_SUBS.has(subName)) {
      const fileName = payload.fileName;
      const fileUrl = payload.fileUrl;
      delete payload.fileName;
      delete payload.fileUrl;
      payload.files = fileUrl ? [{ fileName: fileName || fileUrl, fileUrl }] : [];
    }

    // 과제는 홈/캘린더 화면이 과제 문서에서 직접 courseName을 읽으므로 항상 동봉
    if (subName === "assignments") {
      payload.courseName = course.courseName || "";
    }

    const editingId = form.dataset.editingId;
    if (editingId) {
      await updateDoc(doc(db, "Courses", courseId, subName, editingId), payload);
    } else {
      payload.createdAt = serverTimestamp();
      await addDoc(collection(db, "Courses", courseId, subName), payload);
    }

    resetForm(form, cancelBtn);
    await loadSubItems(courseId, subName, schema, listEl, form);
  });

  return form;
}

function fillForm(form, schema, docId, data) {
  form.dataset.editingId = docId;
  form.querySelector(".btn-cancel-edit").classList.remove("hidden");

  const merged = { ...data };
  if (Array.isArray(data.files) && data.files.length > 0) {
    merged.fileName = data.files[0].fileName || "";
    merged.fileUrl = data.files[0].fileUrl || "";
  }

  for (const f of schema.fields) {
    const val = merged[f.key];
    if (f.type === "date" && val && val.toDate) {
      form.elements[f.key].value = val.toDate().toISOString().slice(0, 10);
    } else {
      form.elements[f.key].value = val ?? "";
    }
  }
  form.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function resetForm(form, cancelBtn) {
  form.reset();
  form.dataset.editingId = "";
  cancelBtn.classList.add("hidden");
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));
}
