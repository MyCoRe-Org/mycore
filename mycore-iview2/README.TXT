Refactor-Plan:


iview[
{
  context: {HTML-Information: Backup, Parent-DIV},
  currentImage: {Aktuelles Bild, aktuelle Darstellung, ZoomLevel etc., Funktionen die nicht in "viewer" liegen},
  thumbnailPanel: {Gallerieübersicht des aktuellen Mets Dokuments},
  chapter: {Kontroller, loaded:Chapter bereits geladen?, parent:Element in das Chapter eingebunden wird}
  overview: {Ov:OverviewController, Model: Model, loaded: Overview bereits geladen?, parent:Element in das Overview eingebunden wird},
  pageOrder: {Paginierung etc.},
  pageStructure: {Chapters},
  pdfCreator: {PDF-funktion},
  permaLink: {},
  properties: {verschiedene Einstellungen pro Instanz},
  toolbar: {mgr, ctrl},
  viewer: {PanonJS-Instanz},
}
]

IVIEW.TILE_SIZE = 256;
IVIEW.START_HEIGHT = 256;
IVIEW.START_WIDTH = 256;