export function isObjectContent(path: string): boolean {
  return path.includes("/contents");
}

export function isObjectContents(path: string): boolean {
  return path.endsWith("/contents");
}

export function isObjectContentFile(path: string): boolean {
  return path.includes("/contents/");
}

export function isDerivate(path: string): boolean {
  return path.includes("/derivates/") && !isObjectContents(path) && !isObjectContentFile(path);
}

export function supportsExpandedObjectView(path: string): boolean {
  return path.split("/").length === 1;
}
