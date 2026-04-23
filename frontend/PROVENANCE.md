# Frontend — vendored from YifanYang6/front-end

Upstream: https://github.com/YifanYang6/front-end
Pinned commit: 64dff7d6adccec497d46a89e223b2172e837ee5f
  — "Configure W3C TraceContext propagators for frontend-backend trace continuity (#7)"

This directory is the Node.js storefront used by the Coherence/Helidon SockShop
demo. It was previously built and published by YifanYang6 as
`opspai/ss-frontend:propagator`. We vendor it here so the fork is self-contained
and a single CI pipeline can produce `ss-frontend` alongside the Java backends
and the Python loadgen.

To pick up a new upstream revision, re-run:

    git clone https://github.com/YifanYang6/front-end /tmp/yf-frontend
    rsync -a --delete --exclude .git /tmp/yf-frontend/ frontend/
    # then update this file with the new commit SHA.
