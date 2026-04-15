CREATE TABLE tasks (
                       id          VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
                       title       VARCHAR(200) NOT NULL,
                       description TEXT,
                       status      VARCHAR(20)  NOT NULL DEFAULT 'todo'
                           CHECK (status IN ('todo', 'in_progress', 'done')),
                       assignee_id VARCHAR(36),
                       created_by  VARCHAR(36)  NOT NULL,
                       due_date    DATE,
                       created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_tasks_assignee   ON tasks (assignee_id);
CREATE INDEX idx_tasks_status     ON tasks (status);
CREATE INDEX idx_tasks_created_at ON tasks (created_at DESC);
CREATE INDEX idx_tasks_due_date   ON tasks (due_date);

CREATE TABLE comments (
                          id         VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
                          task_id    VARCHAR(36)   NOT NULL,
                          author_id  VARCHAR(36)   NOT NULL,
                          text       VARCHAR(1000) NOT NULL,
                          created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_comments_task
                              FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_task   ON comments (task_id);
CREATE INDEX idx_comments_author ON comments (author_id);