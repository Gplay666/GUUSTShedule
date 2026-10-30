const DAYS = {
  1: "Понедельник",
  2: "Вторник",
  3: "Среда",
  4: "Четверг",
  5: "Пятница",
  6: "Суббота",
  7: "Воскресенье",
};

const state = {
  groups: [],
};

const $ = (id) => document.getElementById(id);

function formatWeeks(weeks) {
  if (!Array.isArray(weeks)) return "—";
  const numbers = [...new Set(weeks.map(Number).filter((n) => Number.isInteger(n) && n >= 1))]
    .sort((a, b) => a - b);

  if (!numbers.length) return "—";

  const parts = [];
  let start = numbers[0];
  let previous = numbers[0];

  for (const number of numbers.slice(1)) {
    if (number === previous + 1) {
      previous = number;
      continue;
    }
    parts.push(start === previous ? `${start}` : `${start}–${previous}`);
    start = number;
    previous = number;
  }
  parts.push(start === previous ? `${start}` : `${start}–${previous}`);
  return parts.join(", ");
}

function teacherName(lesson) {
  const full = (lesson.teacher_fullname || "").trim();
  if (full) return full;
  const short = (lesson.teacher || "").trim();
  return short || "—";
}

function roomName(lesson) {
  const short = (lesson.room_title_short || "").trim();
  if (short) return short;
  const parts = [lesson.building_short_title, lesson.room_title]
    .map((part) => (part || "").trim())
    .filter(Boolean);
  return parts.join(", ") || "—";
}

function addCell(row, value) {
  const cell = document.createElement("td");
  cell.textContent = value == null ? "" : String(value);
  row.append(cell);
}

function fillGroupSelect(groups, preferredId = null) {
  const select = $("groupSelect");
  const previous = preferredId ?? select.value;
  select.replaceChildren();

  if (!groups.length) {
    const option = new Option("Ничего не найдено", "");
    option.disabled = true;
    select.add(option);
    return;
  }

  for (const group of groups) {
    const label = group.faculty
      ? `${group.group_title} — ${group.faculty}`
      : group.group_title;
    select.add(new Option(label, String(group.group_id)));
  }

  if (previous && groups.some((group) => String(group.group_id) === String(previous))) {
    select.value = String(previous);
  } else {
    select.value = String(groups[0].group_id);
  }
}

function renderSchedule(data, lessons) {
  const group = state.groups.find((item) => item.group_id === data.group_id);
  $("resultTitle").textContent = group
    ? `${group.group_title} (${group.group_id})`
    : `Группа ${data.group_id}`;
  $("resultMeta").textContent = `Семестр ${data.semester} · занятий: ${lessons.length}`;

  const tbody = $("scheduleBody");
  tbody.replaceChildren();

  for (const lesson of lessons) {
    const row = document.createElement("tr");
    addCell(row, DAYS[lesson.schedule_weekday_id] || `День ${lesson.schedule_weekday_id}`);
    addCell(row, lesson.schedule_time_num ?? "—");
    addCell(row, lesson.schedule_time_title || "—");
    addCell(row, lesson.schedule_subject_title || "—");
    addCell(row, lesson.type || "—");
    addCell(row, teacherName(lesson));
    addCell(row, roomName(lesson));
    addCell(row, formatWeeks(lesson.schedule_weeks));
    tbody.append(row);
  }

  const hasLessons = lessons.length > 0;
  $("result").hidden = false;
  $("tableWrap").hidden = !hasLessons;
  $("empty").hidden = hasLessons;
}

async function loadSchedule(groupId) {
  const status = $("status");
  const button = $("loadBtn");

  button.disabled = true;
  status.classList.remove("error");
  status.textContent = "Загрузка расписания…";
  $("result").hidden = true;
  $("empty").hidden = true;

  try {
    const response = await fetch(`/api/schedule?group_id=${encodeURIComponent(groupId)}`);
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || `HTTP ${response.status}`);
    }

    const rawLessons = Array.isArray(data.lessons) ? data.lessons : [];
    const lessons = rawLessons
      .filter((lesson) => lesson && lesson.schedule_subject_title && Number(lesson.schedule_weekday_id) >= 1)
      .sort((a, b) => {
        const dayDiff = Number(a.schedule_weekday_id) - Number(b.schedule_weekday_id);
        if (dayDiff) return dayDiff;
        const pairDiff = (a.schedule_time_num ?? 99) - (b.schedule_time_num ?? 99);
        if (pairDiff) return pairDiff;
        return String(a.schedule_subject_title).localeCompare(String(b.schedule_subject_title), "ru");
      });

    renderSchedule(data, lessons);
    const group = state.groups.find((item) => item.group_id === data.group_id);
    status.textContent = group
      ? `Показано: ${group.group_title} (${group.group_id}) — ${lessons.length} занятий`
      : `Показано занятий: ${lessons.length}`;
  } catch (error) {
    status.textContent = `Ошибка: ${error.message}`;
    status.classList.add("error");
  } finally {
    button.disabled = false;
  }
}

async function loadGroups() {
  const status = $("status");
  status.classList.remove("error");
  status.textContent = "Загрузка списка групп…";

  try {
    const response = await fetch("/api/groups");
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || `HTTP ${response.status}`);
    }

    state.groups = Array.isArray(data.groups) ? data.groups : [];
    fillGroupSelect(state.groups);
    $("loadBtn").disabled = false;
    status.textContent = `Загружено групп: ${state.groups.length}`;

    const preferred = state.groups.find((group) => group.group_id === 14381);
    if (preferred) {
      $("groupSelect").value = String(preferred.group_id);
      await loadSchedule(preferred.group_id);
    }
  } catch (error) {
    status.textContent = `Не удалось загрузить группы: ${error.message}`;
    status.classList.add("error");
  }
}

let searchTimer = null;
$("groupSearch").addEventListener("input", () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    const query = $("groupSearch").value.trim().toLocaleLowerCase("ru");
    const filtered = query
      ? state.groups.filter((group) => {
          const title = group.group_title.toLocaleLowerCase("ru");
          const faculty = (group.faculty || "").toLocaleLowerCase("ru");
          return title.includes(query) || faculty.includes(query);
        })
      : state.groups;

    fillGroupSelect(filtered);
    $("status").classList.remove("error");
    $("status").textContent = `Найдено групп: ${filtered.length}`;
  }, 120);
});

$("controls").addEventListener("submit", (event) => {
  event.preventDefault();
  const groupId = Number($("groupSelect").value);
  if (Number.isInteger(groupId) && groupId > 0) {
    loadSchedule(groupId);
  }
});

loadGroups();
