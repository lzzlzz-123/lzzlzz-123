import api from "@/api/client";
import type { TimelinePost } from "@/types/post";
import type { TopicResponse } from "@/types/topic";

export interface HotspotPostPayload {
  content: string;
  mediaUrls?: string[];
  topicId?: number | null;
  initialHeat?: number;
}

export interface AdminTopicPayload {
  name: string;
  description?: string | null;
  initialHeat?: number;
}

export const createHotspotPost = async (payload: HotspotPostPayload): Promise<TimelinePost> => {
  const { data } = await api.post<TimelinePost>("/admin/hotspot/posts", {
    content: payload.content,
    mediaUrls: payload.mediaUrls ?? [],
    topicId: payload.topicId ?? null,
    initialHeat: payload.initialHeat ?? 0,
  });
  return data;
};

export const updateHotspotPostHeat = async (postId: number, heat: number): Promise<TimelinePost> => {
  const { data } = await api.put<TimelinePost>(`/admin/hotspot/posts/${postId}/heat`, {
    heat,
  });
  return data;
};

export const createHotTopic = async (payload: AdminTopicPayload): Promise<TopicResponse> => {
  const { data } = await api.post<TopicResponse>("/admin/topics", {
    name: payload.name,
    description: payload.description ?? null,
    initialHeat: payload.initialHeat ?? 0,
  });
  return data;
};

export const updateTopicHeat = async (topicId: number, heat: number): Promise<TopicResponse> => {
  const { data } = await api.put<TopicResponse>(`/admin/topics/${topicId}/heat`, {
    heat,
  });
  return data;
};
