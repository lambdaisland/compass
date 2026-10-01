// Time switch: display session times in the event's own timezone (server
// default) or in the visitor's browser timezone.
//
// - The toggle is a native checkbox with role=switch (#time-mode), hidden
//   behind the ".timeswitch" element unless the browser implements Temporal.
// - State is persisted to localStorage.
// - In "browser" mode, <time> elements with a data-datetime-format attribute
//   have their datetime attribute converted to the browser timezone and their
//   text replaced (format "time" -> "HH:mm", "datetime" -> "Thu 01.10, 08:00").

const TIME_MODE_STORAGE_KEY = "time-mode";

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function temporal_supported() {
  return typeof Temporal !== "undefined"
    && Temporal.Instant
    && Temporal.Now
    && Temporal.Now.timeZoneId;
}

function get_time_switch() {
  return document.getElementById("time-mode");
}

function time_mode() {
  const input = get_time_switch();
  return input && input.checked ? "browser" : "event";
}

function unhide_time_switch() {
  document.querySelectorAll(".timeswitch.hidden").forEach((el) => {
    el.classList.remove("hidden");
  });
}

function pad2(n) {
  return String(n).padStart(2, "0");
}

function format_hh_mm(zdt) {
  return pad2(zdt.hour) + ":" + pad2(zdt.minute);
}

function format_datetime(zdt) {
  return WEEKDAYS[zdt.dayOfWeek - 1] + " " + pad2(zdt.day) + "." + pad2(zdt.month) + ", " + format_hh_mm(zdt);
}

function format_offset(zdt) {
  const offset = zdt.offset;
  const sign = offset.startsWith("-") ? "-" : "+";
  const [hh, mm] = offset.slice(1).split(":").map(Number);
  if (hh === 0 && mm === 0) return "UTC";
  return "UTC" + sign + hh + (mm ? ":" + pad2(mm) : "");
}

function to_browser_zdt(instant_str) {
  return Temporal.Instant.from(instant_str).toZonedDateTimeISO(Temporal.Now.timeZoneId());
}

function convert_time(el) {
  const datetime = el.getAttribute("datetime");
  if (!datetime) return;
  const zdt = to_browser_zdt(datetime);
  const text = el.getAttribute("data-datetime-format") === "datetime"
    ? format_datetime(zdt)
    : format_hh_mm(zdt);
  el.textContent = text;
  el.setAttribute("title", text + " (" + format_offset(zdt) + ")");
}

function apply_time_mode(mode) {
  document.querySelectorAll("time[data-datetime-format]").forEach((el) => {
    if (mode === "browser") {
      if (!el.hasAttribute("data-original-text")) {
        el.setAttribute("data-original-text", el.textContent);
        el.setAttribute("data-original-title", el.getAttribute("title") || "");
      }
      convert_time(el);
    } else if (el.hasAttribute("data-original-text")) {
      el.textContent = el.getAttribute("data-original-text");
      el.setAttribute("title", el.getAttribute("data-original-title") || "");
    }
  });
}

function handle_time_switch_change(e) {
  const mode = e.currentTarget.checked ? "browser" : "event";
  localStorage.setItem(TIME_MODE_STORAGE_KEY, mode);
  apply_time_mode(mode);
}

function ensure_time_switch() {
  if (!temporal_supported()) return;

  unhide_time_switch();

  const input = get_time_switch();
  if (!input) return;

  const stored = localStorage.getItem(TIME_MODE_STORAGE_KEY);
  if (stored) {
    input.checked = stored === "browser";
  }

  input.removeEventListener("change", handle_time_switch_change);
  input.addEventListener("change", handle_time_switch_change);

  apply_time_mode(time_mode());
}

addEventListener("DOMContentLoaded", ensure_time_switch);
addEventListener("htmx:afterSwap", (_) => ensure_time_switch());
addEventListener("popstate", () => setTimeout(ensure_time_switch, 0));

// Local Variables:
// js-indent-level: 2
// End:
