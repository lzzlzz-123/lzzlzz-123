export interface TimelinePost {
  id: number;
  content: string;
  mediaUrls: string[];
  createdAt: string;
  updatedAt: string;
  topic?: {
    id: number;
    name: string;
  } | null;
  author: {
    id: number;
    username: string;
    displayName: string;
    avatarUrl?: string | null;
  };
  likeCount: number;
  commentCount: number;
  heat: number;
  inHotspot: boolean;
  likedByCurrentUser: boolean;
}
