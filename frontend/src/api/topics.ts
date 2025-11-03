import api from "@/api/client";
import type { PagedResponse } from "@/types/pagination";
import type { TimelinePost } from "@/types/post";
import type { TopicResponse, TopicSummary } from "@/types/topic";

export const fetchTopicRankings = async (size = 20): Promise<TopicSummary[]> => {
  const { data } = await api.get<TopicSummary[]>("/topics/rankings", {
    params: { size },
  });
  return data;
};

export const fetchMyTopics = async (): Promise<TopicSummary[]> => {
  const { data } = await api.get<TopicSummary[]>("/topics/mine");
  return data;
};

export const createTopic = async (payload: { name: string; description?: string | null }): Promise<TopicResponse> => {
  const { data } = await api.post<TopicResponse>("/topics", payload);
  return data;
};

export const joinTopic = async (topicId: number): Promise<TopicResponse> => {
  const { data } = await api.post<TopicResponse>(`/topics/${topicId}/join`);
  return data;
};

export const leaveTopic = async (topicId: number): Promise<void> => {
  await api.delete(`/topics/${topicId}/join`);
};

export const fetchTopic = async (topicId: number): Promise<TopicResponse> => {
  const { data } = await api.get<TopicResponse>(`/topics/${topicId}`);
  return data;
};

export const fetchTopicPosts = async (
  topicId: number,
  page = 0,
  size = 20
): Promise<PagedResponse<TimelinePost>> => {
  const { data } = await api.get<PagedResponse<TimelinePost>>(`/topics/${topicId}/posts`, {
    params: { page, size },
  });
  return data;
};
