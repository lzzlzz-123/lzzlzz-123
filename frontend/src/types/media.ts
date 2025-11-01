export interface UploadedMedia {
  url: string;
  contentType: string;
  mediaType: "image" | "video" | "unknown";
  size: number;
  originalFilename?: string | null;
}
