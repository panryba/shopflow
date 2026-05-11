export interface Product {
  id: string;
  artist: string;
  name: string;
  price: number;
  image: string;
}

export const PRODUCTS: Product[] = [
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000002',
    artist: 'Pink Floyd',
    name: 'Animals',
    price: 36.99,
    image: '/assets/covers/pink-floyd-animals.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000003',
    artist: 'Black Sabbath',
    name: 'Vol. 4',
    price: 31.99,
    image: '/assets/covers/black-sabbath-vol4.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000004',
    artist: 'The Beatles',
    name: 'Abbey Road',
    price: 38.99,
    image: '/assets/covers/beatles-abbey-road.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000001',
    artist: 'Led Zeppelin',
    name: 'IV',
    price: 34.99,
    image: '/assets/covers/led-zeppelin-iv.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000005',
    artist: 'King Crimson',
    name: 'In the Court of the Crimson King',
    price: 39.99,
    image: '/assets/covers/king-crimson-in-the-court.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000006',
    artist: 'AC/DC',
    name: 'Let There Be Rock',
    price: 29.99,
    image: '/assets/covers/acdc-let-there-be-rock.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000007',
    artist: 'Thin Lizzy',
    name: 'Jailbreak',
    price: 32.99,
    image: '/assets/covers/thin-lizzy-jailbreak.jpg'
  },
  {
    id: 'a1b2c3d4-0001-0001-0001-000000000008',
    artist: 'Dire Straits',
    name: 'Making Movies',
    price: 33.99,
    image: '/assets/covers/dire-straits-making-movies.jpg'
  }
];

export const PRODUCT_MAP = new Map(PRODUCTS.map(p => [p.id, p]));
