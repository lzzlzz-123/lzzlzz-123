export interface TimelinePost {
  id: number;
  content: string;
  mediaUrls: string[];
  createdAt: string;
  updatedAt: string;
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
