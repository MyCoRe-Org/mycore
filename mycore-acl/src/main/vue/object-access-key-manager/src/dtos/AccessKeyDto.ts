export default interface AccessKeyDto {
  id?: string,
  reference?: string;
  value?: string;
  permission?: string;
  comment?: string;
  expiration?: number | null;
}