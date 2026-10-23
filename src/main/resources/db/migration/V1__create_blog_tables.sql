CREATE TABLE blog_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_blog_users_email UNIQUE (email),
    CONSTRAINT ck_blog_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE blog_posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    excerpt VARCHAR(500),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    author_id BIGINT NOT NULL REFERENCES blog_users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_blog_posts_slug UNIQUE (slug),
    CONSTRAINT ck_blog_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_blog_posts_status_created_at ON blog_posts (status, created_at DESC);
CREATE INDEX idx_blog_posts_author_id ON blog_posts (author_id);

CREATE TABLE blog_comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL REFERENCES blog_posts (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES blog_users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_blog_comments_post_created_at ON blog_comments (post_id, created_at);
