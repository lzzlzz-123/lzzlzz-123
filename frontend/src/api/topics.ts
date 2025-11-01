import api from "@/api/client";
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
