export type PostVisibility = "PUBLIC" | "FOLLOWERS_ONLY" | "PRIVATE" | "CUSTOM";

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
    admin: boolean;
  };
  likeCount: number;
  commentCount: number;
  heat: number;
  inHotspot: boolean;
  likedByCurrentUser: boolean;
  ownedByCurrentUser: boolean;
  visibility: PostVisibility;
  allowedUserIds: number[];
}
