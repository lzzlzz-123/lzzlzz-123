import api from "@/api/client";
import type { UploadedMedia } from "@/types/media";

export const uploadMedia = async (files: File[]): Promise<UploadedMedia[]> => {
  if (!files.length) {
    return [];
  }
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));
  const { data } = await api.post<UploadedMedia[]>("/media/upload", formData);
  return data;
};
