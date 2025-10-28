"""
Load Generator for Coherence Helidon Sock Shop

This load generator simulates realistic e-commerce user behavior with configurable
load patterns and API call ratios.
"""

import base64
import json
import random
import os
from locust import HttpUser, TaskSet, task, between
from locust.exception import StopUser


class SockShopUser(TaskSet):
    """
    Simulates a user shopping on the Sock Shop application.
    Task weights reflect realistic e-commerce behavior patterns.
    """

    def on_start(self):
        """Initialize user session with catalog data"""
        self.catalogue = []
        self.user_id = None
        self.cart_id = None
        
        # Load catalogue on start
        response = self.client.get("/catalogue")
        if response.status_code == 200:
            self.catalogue = response.json()
        
    @task(40)
    def browse_catalogue(self):
        """
        Browse product catalogue (40% of requests)
        Most common user action - just browsing
        """
        page = random.randint(1, 3)
        size = random.randint(3, 12)
        tags = random.choice(['', 'blue', 'green', 'formal', 'sport'])
        
        params = {
            'page': page,
            'size': size
        }
        if tags:
            params['tags'] = tags
            
        self.client.get("/catalogue", params=params, name="/catalogue")
    
    @task(30)
    def view_product_details(self):
        """
        View individual product details (30% of requests)
        Second most common action - users interested in specific items
        """
        if not self.catalogue:
            return
            
        product = random.choice(self.catalogue)
        product_id = product.get('id')
        
        if product_id:
            self.client.get(f"/catalogue/{product_id}", name="/catalogue/[id]")
            
            # Sometimes view the product image
            if random.random() < 0.5:
                image_url = product.get('imageUrl', [''])[0]
                if image_url:
                    self.client.get(f"/catalogue{image_url}", name="/catalogue/images/[image]")
    
    @task(10)
    def user_login_flow(self):
        """
        User registration and login flow (10% of requests)
        Moderate frequency - new users or returning users logging in
        """
        # Generate random user credentials
        user_num = random.randint(1, 10000)
        username = f"user{user_num}"
        password = "password"
        
        # Try to login first
        credentials = base64.b64encode(f"{username}:{password}".encode()).decode()
        response = self.client.get(
            "/login",
            headers={"Authorization": f"Basic {credentials}"},
            name="/login"
        )
        
        # If login fails, try to register (simulates new user)
        if response.status_code == 401 and random.random() < 0.3:
            user_data = {
                "username": username,
                "password": password,
                "email": f"{username}@example.com",
                "firstName": f"First{user_num}",
                "lastName": f"Last{user_num}"
            }
            
            self.client.post(
                "/register",
                json=user_data,
                name="/register"
            )
            
            # Try login again after registration
            self.client.get(
                "/login",
                headers={"Authorization": f"Basic {credentials}"},
                name="/login"
            )
        
        if response.status_code == 200:
            self.user_id = username
    
    @task(15)
    def shopping_cart_operations(self):
        """
        Shopping cart operations (15% of requests)
        Add, view, update, and remove items from cart
        """
        if not self.catalogue:
            return
            
        # Use session-based cart ID if user not logged in
        cart_id = self.user_id if self.user_id else f"session{random.randint(1, 1000)}"
        
        # Get current cart
        response = self.client.get(f"/carts/{cart_id}", name="/carts/[cartId]")
        
        # Add item to cart (most common cart operation)
        if random.random() < 0.7:
            product = random.choice(self.catalogue)
            item_data = {
                "itemId": product.get('id'),
                "unitPrice": product.get('price'),
                "quantity": random.randint(1, 3)
            }
            
            self.client.post(
                f"/carts/{cart_id}/items",
                json=item_data,
                name="/carts/[cartId]/items"
            )
        
        # View cart items
        self.client.get(f"/carts/{cart_id}/items", name="/carts/[cartId]/items")
        
        # Sometimes update or delete items
        if response.status_code == 200:
            cart = response.json()
            items = cart.get('items', [])
            
            if items and random.random() < 0.2:
                item = random.choice(items)
                item_id = item.get('itemId')
                
                # Update or delete
                if random.random() < 0.5:
                    # Update quantity
                    item['quantity'] = random.randint(1, 5)
                    self.client.patch(
                        f"/carts/{cart_id}/items",
                        json=item,
                        name="/carts/[cartId]/items PATCH"
                    )
                else:
                    # Delete item
                    self.client.delete(
                        f"/carts/{cart_id}/items/{item_id}",
                        name="/carts/[cartId]/items/[itemId]"
                    )
    
    @task(5)
    def place_order(self):
        """
        Place an order (5% of requests)
        Lowest frequency - represents actual conversions
        Only a small percentage of users complete purchase
        """
        if not self.user_id:
            # Need to be logged in to place order
            return
            
        cart_id = self.user_id
        
        # Get cart items
        response = self.client.get(f"/carts/{cart_id}/items", name="/carts/[cartId]/items")
        
        if response.status_code != 200:
            return
            
        items = response.json()
        if not items:
            # Cart is empty, add an item first
            if self.catalogue:
                product = random.choice(self.catalogue)
                item_data = {
                    "itemId": product.get('id'),
                    "unitPrice": product.get('price'),
                    "quantity": 1
                }
                self.client.post(
                    f"/carts/{cart_id}/items",
                    json=item_data,
                    name="/carts/[cartId]/items"
                )
        
        # Get or create customer info
        customer_response = self.client.get(f"/customers/{self.user_id}", name="/customers/[id]")
        
        if customer_response.status_code != 200:
            # Create customer
            customer_data = {
                "id": self.user_id,
                "firstName": f"First{self.user_id}",
                "lastName": f"Last{self.user_id}",
                "username": self.user_id
            }
            self.client.post("/customers", json=customer_data, name="/customers")
        
        # Add address if needed
        address_response = self.client.get(f"/customers/{self.user_id}/addresses", name="/customers/[id]/addresses")
        address_id = None
        
        if address_response.status_code == 200:
            addresses = address_response.json().get('_embedded', {}).get('address', [])
            if addresses:
                address_id = addresses[0].get('id')
        
        if not address_id:
            address_data = {
                "street": "123 Main St",
                "city": "Springfield",
                "postcode": "12345",
                "country": "USA"
            }
            addr_resp = self.client.post(
                f"/customers/{self.user_id}/addresses",
                json=address_data,
                name="/customers/[id]/addresses"
            )
            if addr_resp.status_code == 200:
                address_id = addr_resp.json().get('id')
        
        # Add card if needed
        card_response = self.client.get(f"/customers/{self.user_id}/cards", name="/customers/[id]/cards")
        card_id = None
        
        if card_response.status_code == 200:
            cards = card_response.json().get('_embedded', {}).get('card', [])
            if cards:
                card_id = cards[0].get('id')
        
        if not card_id:
            card_data = {
                "longNum": "1234567890123456",
                "expires": "12/25",
                "ccv": "123"
            }
            card_resp = self.client.post(
                f"/customers/{self.user_id}/cards",
                json=card_data,
                name="/customers/[id]/cards"
            )
            if card_resp.status_code == 200:
                card_id = card_resp.json().get('id')
        
        # Place the order
        if address_id and card_id:
            order_data = {
                "customer": {"href": f"/customers/{self.user_id}"},
                "address": {"href": f"/addresses/{address_id}"},
                "card": {"href": f"/cards/{card_id}"},
                "items": {"href": f"/carts/{cart_id}/items"}
            }
            
            self.client.post("/orders", json=order_data, name="/orders")


class WebsiteUser(HttpUser):
    """
    Represents a user visiting the Sock Shop website.
    Configurable via environment variables.
    """
    tasks = [SockShopUser]
    
    # Wait time between tasks (simulates user think time)
    # Can be overridden by LOCUST_MIN_WAIT and LOCUST_MAX_WAIT environment variables
    wait_time = between(
        float(os.getenv('LOCUST_MIN_WAIT', '1')),
        float(os.getenv('LOCUST_MAX_WAIT', '3'))
    )
