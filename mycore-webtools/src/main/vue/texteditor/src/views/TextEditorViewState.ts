export const EXPANDED_VIEW_PREFERENCE_KEY = "mycore.texteditor.objectView.expanded";

export function readExpandedViewPreference(storage: Storage | undefined): boolean {
  try {
    return storage?.getItem(EXPANDED_VIEW_PREFERENCE_KEY) === "true";
  } catch {
    return false;
  }
}

export function writeExpandedViewPreference(storage: Storage | undefined, expanded: boolean): void {
  try {
    storage?.setItem(EXPANDED_VIEW_PREFERENCE_KEY, String(expanded));
  } catch {
    // Storage persistence is best effort; blocked storage must not break the editor.
  }
}

export function isUpdateButtonVisible(writeAccess: boolean, isContents: boolean): boolean {
  return writeAccess && !isContents;
}

export function isUpdateButtonDisabled(updateEnabled: boolean, expandedView: boolean): boolean {
  return expandedView || !updateEnabled;
}

export function viewModeButtonClass(active: boolean): string {
  return active ? "btn-primary active" : "btn-outline-primary";
}

export function resolveExpandedViewOnNavigation(currentExpandedView: boolean, supportsExpandedView: boolean): boolean {
  return supportsExpandedView && currentExpandedView;
}
