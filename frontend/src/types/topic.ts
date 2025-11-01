export interface TopicSummary {
  id: number;
  name: string;
  description: string | null;
  heat: number;
  memberCount: number;
  joined: boolean;
}

export interface TopicOwnerSummary {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
  admin: boolean;
}

export interface TopicResponse extends TopicSummary {
  owner: TopicOwnerSummary;
  createdAt: string;
}
