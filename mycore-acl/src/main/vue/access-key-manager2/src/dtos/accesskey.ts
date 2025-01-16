export interface AccessKeyDto {
  id: string;
  reference: string;
  secret: string;
  permission: string;
  isActive: boolean;
  comment?: string;
  expiration?: number | null;
}

export interface PartialUpdateAccessKeyDto {
  reference?: string;
  permission?: string;
  isActive?: boolean;
  comment?: string;
  expiration?: number | null;
}

export interface CreateAccessKeyDto {
  reference: string;
  secret: string;
  permission: string;
  isActive: boolean;
  comment?: string;
  expiration?: number | null;
}
