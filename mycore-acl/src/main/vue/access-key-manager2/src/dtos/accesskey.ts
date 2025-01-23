export interface AccessKeyDto {
  id: string;
  reference: string;
  secret: string;
  type: string;
  isActive: boolean;
  comment?: string;
  expiration?: number | undefined;
}

export interface PartialUpdateAccessKeyDto {
  reference?: string;
  type?: string;
  isActive?: boolean;
  comment?: string;
  expiration?: number | null;
}

export interface CreateAccessKeyDto {
  reference: string;
  secret: string;
  type: string;
  isActive: boolean;
  comment?: string;
  expiration?: number | null;
}
