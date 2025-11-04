"""
Load Generator for Coherence Helidon Sock Shop

This load generator simulates realistic e-commerce user behavior with configurable
load patterns and API call ratios. Based on the original sockshop load test pattern.
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
    Follows the original sockshop load test API structure.
    """

    def on_start(self):
        """Initialize user session with catalog data"""
        self.catalogue = []
        self.logged_in = False
        self.username = None
        
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
        # Get catalogue
        response = self.client.get("/catalogue", name="/catalogue")
        if response.status_code == 200:
            self.catalogue = response.json()
        
        # Sometimes view category page
        if random.random() < 0.3:
            self.client.get("/category.html", name="/category.html")
    
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
            # View product detail page
            self.client.get(f"/detail.html?id={product_id}", name="/detail.html?id=[id]")
            
            # Get product from API
            self.client.get(f"/catalogue/{product_id}", name="/catalogue/[id]")
            
            # Sometimes view the product image
            if random.random() < 0.5:
                image_urls = product.get('imageUrl', [])
                if image_urls and len(image_urls) > 0:
                    image_url = image_urls[0] if isinstance(image_urls, list) else image_urls
                    # Image URL is like /catalogue/images/xxx.jpg
                    self.client.get(image_url, name="/catalogue/images/[image]")
    
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
        
        # Register first if it's a new user (50% chance)
        # This reduces 401 errors during testing
        if random.random() < 0.5:
            user_data = {
                "username": username,
                "password": password,
                "email": f"{username}@example.com",
                "firstName": f"First{user_num}",
                "lastName": f"Last{user_num}"
            }
            
            # Register (no-op if user already exists)
            with self.client.post("/register", json=user_data, name="/register", catch_response=True) as response:
                # 200 = new user, 409 = already exists (both are OK)
                if response.status_code in [200, 409]:
                    response.success()
        
        # Try to login with Basic Auth
        credentials = base64.b64encode(f"{username}:{password}".encode()).decode()
        with self.client.get(
            "/login",
            headers={"Authorization": f"Basic {credentials}"},
            name="/login",
            catch_response=True
        ) as response:
            if response.status_code == 200:
                self.logged_in = True
                self.username = username
                response.success()
            elif response.status_code == 401:
                # 401 is expected for non-existent users, don't count as failure
                response.success()
    
    @task(15)
    def shopping_cart_operations(self):
        """
        Shopping cart operations (15% of requests)
        Add, view, and manage items in cart
        Uses /cart endpoint (singular) as per original sockshop
        """
        if not self.catalogue:
            return
        
        # Select a random product
        product = random.choice(self.catalogue)
        item_id = product.get('id')
        
        # View basket page (where cart is displayed)
        self.client.get("/basket.html", name="/basket.html")
        
        # Add item to cart (most common cart operation - 70% chance)
        if random.random() < 0.7 and item_id:
            item_data = {
                "id": item_id,
                "quantity": random.randint(1, 3)
            }
            
            self.client.post(
                "/cart",
                json=item_data,
                name="/cart POST"
            )
        
        # Sometimes delete cart (20% chance - simulates clearing cart)
        # Note: 404 response is normal when cart doesn't exist
        if random.random() < 0.2:
            with self.client.delete("/cart", name="/cart DELETE", catch_response=True) as response:
                if response.status_code in [202, 404]:
                    # 202 = deleted, 404 = didn't exist (both are OK)
                    response.success()
    
    @task(5)
    def place_order(self):
        """
        Complete purchase flow (5% of requests)
        Simulates the full user journey from browsing to order placement
        Note: This calls the frontend /orders endpoint which internally handles
        customer/address/card creation via the frontend logic
        """
        if not self.catalogue:
            return
        
        # Select a product
        product = random.choice(self.catalogue)
        item_id = product.get('id')
        
        if not item_id:
            return
        
        # Simulate full user journey
        # 1. Visit home page
        self.client.get("/", name="/")
        
        # 2. Browse catalogue
        self.client.get("/catalogue", name="/catalogue")
        
        # 3. View category page
        self.client.get("/category.html", name="/category.html")
        
        # 4. View product detail
        self.client.get(f"/detail.html?id={item_id}", name="/detail.html?id=[id]")
        
        # 5. Login/Register to have a user account (required for orders)
        username = f"user{random.randint(1, 10000)}"
        password = "password"
        credentials = base64.b64encode(f"{username}:{password}".encode()).decode()
        
        # Try to register first to ensure user exists
        user_data = {
            "username": username,
            "password": password,
            "email": f"{username}@example.com",
            "firstName": f"First{username[-4:]}",
            "lastName": f"Last{username[-4:]}"
        }
        self.client.post("/register", json=user_data, name="/register", catch_response=True)
        
        # Login
        login_response = self.client.get(
            "/login",
            headers={"Authorization": f"Basic {credentials}"},
            name="/login"
        )
        
        if login_response.status_code != 200:
            # Login failed, skip order
            return
        
        # 6. Create address for the user (required for orders)
        address_data = {
            "number": "123",
            "street": "Main St",
            "city": "Springfield", 
            "postcode": "12345",
            "country": "USA"
        }
        self.client.post(
            f"/customers/{username}/addresses",
            json=address_data,
            name="/customers/[id]/addresses POST",
            catch_response=True
        )
        
        # 7. Create card for the user (required for orders)
        card_data = {
            "longNum": "1234567890123456",
            "expires": "12/25",
            "ccv": "123"
        }
        self.client.post(
            f"/customers/{username}/cards",
            json=card_data,
            name="/customers/[id]/cards POST",
            catch_response=True
        )
        
        # 8. Clear cart
        self.client.delete("/cart", name="/cart DELETE", catch_response=True)
        
        # 9. Add item to cart
        item_data = {
            "id": item_id,
            "quantity": 1
        }
        self.client.post("/cart", json=item_data, name="/cart POST")
        
        # 10. View basket
        self.client.get("/basket.html", name="/basket.html")
        
        # 11. Place order (frontend handles the complex order creation)
        self.client.post("/orders", name="/orders")


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
