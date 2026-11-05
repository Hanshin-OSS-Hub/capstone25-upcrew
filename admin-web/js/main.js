import { setOnAdminReady } from "./auth.js";
import { initUsersTab } from "./users.js";
import { initCoursesTab } from "./courses.js";
import { initReservationsTab } from "./reservations.js";

const tabButtons = document.querySelectorAll(".tab-btn");
const tabPanels = document.querySelectorAll(".tab-panel");

const TAB_INITIALIZERS = {
  users: initUsersTab,
  courses: initCoursesTab,
  reservations: initReservationsTab
};

const loadedTabs = new Set();

function activateTab(tabName) {
  tabButtons.forEach((btn) => btn.classList.toggle("active", btn.dataset.tab === tabName));
  tabPanels.forEach((panel) => panel.classList.toggle("hidden", panel.id !== `tab-${tabName}`));

  if (!loadedTabs.has(tabName)) {
    loadedTabs.add(tabName);
    TAB_INITIALIZERS[tabName]();
  }
}

tabButtons.forEach((btn) => {
  btn.addEventListener("click", () => activateTab(btn.dataset.tab));
});

setOnAdminReady(() => {
  loadedTabs.clear();
  activateTab("users");
});
