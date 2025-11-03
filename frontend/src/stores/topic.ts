import { defineStore } from "pinia";
import {
  createTopic as apiCreateTopic,
  fetchMyTopics as apiFetchMyTopics,
  fetchTopicRankings as apiFetchTopicRankings,
  joinTopic as apiJoinTopic,
  leaveTopic as apiLeaveTopic,
} from "@/api/topics";
import type { TopicResponse, TopicSummary } from "@/types/topic";

interface TopicState {
  rankings: TopicSummary[];
  rankingsLoading: boolean;
  rankingsLoaded: boolean;
  rankingsError: string | null;
  myTopics: TopicSummary[];
  myTopicsLoading: boolean;
  myTopicsLoaded: boolean;
  myTopicsError: string | null;
}

const toSummary = (topic: TopicSummary | TopicResponse): TopicSummary => ({
  id: topic.id,
  name: topic.name,
  description: topic.description,
  heat: topic.heat,
  memberCount: topic.memberCount,
  joined: topic.joined,
});

const TOPIC_RANKING_LIMIT = 20;

const sortTopics = (topics: TopicSummary[]): TopicSummary[] =>
  [...topics].sort((a, b) => {
    if (b.heat !== a.heat) {
      return b.heat - a.heat;
    }
    if (b.memberCount !== a.memberCount) {
      return b.memberCount - a.memberCount;
    }
    return a.id - b.id;
  });

const updateCollection = (collection: TopicSummary[], summary: TopicSummary): TopicSummary[] => {
  const index = collection.findIndex((item) => item.id === summary.id);
  if (index >= 0) {
    const clone = collection.slice();
    clone[index] = summary;
    return clone;
  }
  return [summary, ...collection];
};

export const useTopicStore = defineStore("topic", {
  state: (): TopicState => ({
    rankings: [],
    rankingsLoading: false,
    rankingsLoaded: false,
    rankingsError: null,
    myTopics: [],
    myTopicsLoading: false,
    myTopicsLoaded: false,
    myTopicsError: null,
  }),
  actions: {
    async fetchRankings(force = false) {
      if (this.rankingsLoading) return;
      if (this.rankingsLoaded && !force) return;
      this.rankingsLoading = true;
      this.rankingsError = null;
      try {
        const data = await apiFetchTopicRankings(TOPIC_RANKING_LIMIT);
        this.rankings = sortTopics(data.map(toSummary)).slice(0, TOPIC_RANKING_LIMIT);
        this.rankingsLoaded = true;
      } catch (error: any) {
        this.rankingsError = error?.response?.data?.message ?? "加载话题热度榜失败";
      } finally {
        this.rankingsLoading = false;
      }
    },
    async fetchMyTopics(force = false) {
      if (this.myTopicsLoading) return;
      if (this.myTopicsLoaded && !force) return;
      this.myTopicsLoading = true;
      this.myTopicsError = null;
      try {
        const data = await apiFetchMyTopics();
        this.myTopics = data.map(toSummary);
        this.myTopicsLoaded = true;
      } catch (error: any) {
        this.myTopicsError = error?.response?.data?.message ?? "加载我的话题失败";
      } finally {
        this.myTopicsLoading = false;
      }
    },
    resetMyTopics() {
      this.myTopics = [];
      this.myTopicsLoaded = false;
      this.myTopicsError = null;
    },
    async createTopic(payload: { name: string; description?: string | null }): Promise<TopicResponse> {
      const data = await apiCreateTopic(payload);
      const summary = toSummary(data);
      this.myTopics = updateCollection(this.myTopics, summary);
      this.updateRanking(summary);
      return data;
    },
    async joinTopic(topicId: number): Promise<TopicResponse> {
      const data = await apiJoinTopic(topicId);
      const summary = toSummary(data);
      this.myTopics = updateCollection(this.myTopics, summary);
      this.updateRanking(summary);
      return data;
    },
    async leaveTopic(topicId: number) {
      await apiLeaveTopic(topicId);
      this.myTopics = this.myTopics.filter((topic) => topic.id !== topicId);
      const existing = this.rankings.find((topic) => topic.id === topicId);
      if (existing) {
        this.updateRanking({ ...existing, joined: false });
      }
    },
    updateRanking(summary: TopicSummary) {
      const map = new Map<number, TopicSummary>();
      this.rankings.forEach((topic) => {
        map.set(topic.id, topic);
      });
      map.set(summary.id, summary);
      this.rankings = sortTopics(Array.from(map.values())).slice(0, TOPIC_RANKING_LIMIT);
    },
  },
});
