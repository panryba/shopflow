export interface Product {
  id: string;
  artist: string;
  title: string;
  price: number;
  imageUrl: string;
}

export interface ImportResult {
  imported: number;
  skipped: number;
  skippedRecords: string[];
}