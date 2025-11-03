export type PrivacySetting = "PUBLIC" | "FOLLOWERS_ONLY" | "PRIVATE";

export interface UserSummary {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
  admin: boolean;
}

export interface UserConnections {
  followers: UserSummary[];
  followees: UserSummary[];
}
