import { initializeApp } from "https://www.gstatic.com/firebasejs/13.2.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/13.2.0/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/13.2.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyADyAD0bTaSr_Sqlhr_RnHFQ08Me6XR-VI",
  authDomain: "capstone-upcrew.firebaseapp.com",
  databaseURL: "https://capstone-upcrew-default-rtdb.firebaseio.com",
  projectId: "capstone-upcrew",
  storageBucket: "capstone-upcrew.firebasestorage.app",
  messagingSenderId: "571354914599",
  appId: "1:571354914599:web:c057e6136b5feeaef51302",
  measurementId: "G-DKQXRYYEZK"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
