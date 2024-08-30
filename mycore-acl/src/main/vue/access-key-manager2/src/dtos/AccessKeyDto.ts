export default interface AccessKeyDto {
  objectId?: string;
  value?: string;
  permission?: string;
  comment?: string;
  expiration?: number | null;
}