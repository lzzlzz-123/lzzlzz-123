export type PrivacySetting = "PUBLIC" | "FOLLOWERS_ONLY" | "PRIVATE";

export interface UserSummary {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
  admin: boolean;
}

export interface UserProfile {
  id: number;
  username: string;
  displayName: string;
  email: string;
  bio?: string | null;
  signature?: string | null;
  location?: string | null;
  avatarUrl?: string | null;
  privacySetting: PrivacySetting;
  createdAt: string;
  followerCount: number;
  followingCount: number;
  postCount: number;
  followedByCurrentUser: boolean;
}

export interface UserConnections {
  followers: UserSummary[];
  followees: UserSummary[];
}
