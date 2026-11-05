import {
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/13.2.0/firebase-auth.js";
import { doc, getDoc } from "https://www.gstatic.com/firebasejs/13.2.0/firebase-firestore.js";
import { auth, db } from "./firebase-config.js";

const loginSection = document.getElementById("login-section");
const appSection = document.getElementById("app-section");
const loginForm = document.getElementById("login-form");
const loginError = document.getElementById("login-error");
const logoutBtn = document.getElementById("logout-btn");
const adminNameEl = document.getElementById("admin-name");

let onAdminReady = null;

export function setOnAdminReady(callback) {
  onAdminReady = callback;
}

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  loginError.textContent = "";

  const email = document.getElementById("login-email").value.trim();
  const password = document.getElementById("login-password").value;

  try {
    await signInWithEmailAndPassword(auth, email, password);
  } catch (err) {
    loginError.textContent = "로그인 실패: " + err.message;
  }
});

logoutBtn.addEventListener("click", async () => {
  await signOut(auth);
});

onAuthStateChanged(auth, async (user) => {
  if (!user) {
    appSection.classList.add("hidden");
    loginSection.classList.remove("hidden");
    return;
  }

  const userDoc = await getDoc(doc(db, "Users", user.uid));

  if (!userDoc.exists() || userDoc.data().isAdmin !== true) {
    loginError.textContent = "관리자 권한이 없는 계정입니다.";
    await signOut(auth);
    return;
  }

  adminNameEl.textContent = userDoc.data().name || user.email;
  loginSection.classList.add("hidden");
  appSection.classList.remove("hidden");

  if (onAdminReady) onAdminReady(user, userDoc.data());
});
