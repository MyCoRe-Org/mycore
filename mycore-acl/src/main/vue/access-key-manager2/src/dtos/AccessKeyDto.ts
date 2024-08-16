export default interface AccessKeyDto {
  id?: string,
  reference: string;
  value: string;
  permission: string;
  isActive: boolean;
  comment?: string;
  expiration?: number | null;
}
