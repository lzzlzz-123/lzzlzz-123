import api from "@/api/client";
import type { HomeAd } from "@/types/homeAd";

export interface HomeAdPayload {
  title: string;
  imageUrl: string;
  targetUrl: string;
  displayOrder?: number;
  active?: boolean;
}

const normalizePayload = (payload: HomeAdPayload) => {
  const title = (payload.title ?? "").trim();
  const imageUrl = (payload.imageUrl ?? "").trim();
  const targetUrl = (payload.targetUrl ?? "").trim();
  const rawOrder = Number(payload.displayOrder ?? 0);
  const displayOrder = Number.isFinite(rawOrder) ? Math.max(0, Math.floor(rawOrder)) : 0;
  const active = payload.active ?? true;
  return {
    title,
    imageUrl,
    targetUrl,
    displayOrder,
    active,
  };
};

export const fetchHomeAds = async (): Promise<HomeAd[]> => {
  const { data } = await api.get<HomeAd[]>("/home-ads");
  return Array.isArray(data) ? data : [];
};

export const fetchAdminHomeAds = async (): Promise<HomeAd[]> => {
  const { data } = await api.get<HomeAd[]>("/admin/home-ads");
  return Array.isArray(data) ? data : [];
};

export const createHomeAd = async (payload: HomeAdPayload): Promise<HomeAd> => {
  const { data } = await api.post<HomeAd>("/admin/home-ads", normalizePayload(payload));
  return data;
};

export const updateHomeAd = async (adId: number, payload: HomeAdPayload): Promise<HomeAd> => {
  const { data } = await api.put<HomeAd>(`/admin/home-ads/${adId}`, normalizePayload(payload));
  return data;
};

export const deleteHomeAd = async (adId: number): Promise<void> => {
  await api.delete(`/admin/home-ads/${adId}`);
};
