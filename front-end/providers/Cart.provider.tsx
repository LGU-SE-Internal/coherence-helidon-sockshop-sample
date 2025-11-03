// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

import { createContext, useCallback, useContext, useMemo, useState } from 'react';

interface CartItem {
  productId: string;
  quantity: number;
}

interface IProductCart {
  userId: string;
  items: CartItem[];
}

interface IContext {
  cart: IProductCart;
  addItem(item: CartItem): void;
  emptyCart(): void;
}

export const Context = createContext<IContext>({
  cart: { userId: '', items: [] },
  addItem: () => {},
  emptyCart: () => {},
});

interface IProps {
  children: React.ReactNode;
}

export const useCart = () => useContext(Context);

const CartProvider = ({ children }: IProps) => {
  const [cart, setCart] = useState<IProductCart>({ userId: '', items: [] });

  const addItem = useCallback((item: CartItem) => {
    setCart(prev => ({
      ...prev,
      items: [...prev.items, item]
    }));
  }, []);

  const emptyCart = useCallback(() => {
    setCart(prev => ({ ...prev, items: [] }));
  }, []);

  const value = useMemo(() => ({ cart, addItem, emptyCart }), [cart, addItem, emptyCart]);

  return <Context.Provider value={value}>{children}</Context.Provider>;
};

export default CartProvider;
