export default interface PartialUpdateAccessKeyDto {
    reference?: string;
    permission?: string;
    isActive?: boolean;
    comment?: string;
    expiration?: number | null;
  }
  