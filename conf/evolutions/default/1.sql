-- schema

-- !Ups
CREATE TABLE headers
(
    id        VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64) NOT NULL,
    height    INTEGER     NOT NULL,
    timestamp BIGINT      NOT NULL,
    PRIMARY KEY (id)
);


CREATE INDEX "headers__parent_id" ON headers (parent_id);
CREATE INDEX "headers__height" ON headers (height);
CREATE INDEX "headers__ts" ON headers (timestamp);

CREATE TABLE headers_fork
(
    id        VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64) NOT NULL,
    height    INTEGER     NOT NULL,
    timestamp BIGINT      NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX "headers_fork_f__parent_id" ON headers_fork (parent_id);
CREATE INDEX "headers_fork_f__height" ON headers_fork (height);
CREATE INDEX "headers_fork_f__ts" ON headers_fork (timestamp);



CREATE TABLE outputs
(
    box_id          VARCHAR(64)  NOT NULL,
    tx_id           VARCHAR(64)  NOT NULL,
    header_id       VARCHAR(64)  NOT NULL,
    value           BIGINT       NOT NULL,
    creation_height INTEGER      NOT NULL,
    index           INTEGER      NOT NULL,
    ergo_tree       VARCHAR      NOT NULL,
    timestamp       BIGINT       NOT NULL,
    bytes           BLOB         NOT NULL,
    spent           BOOLEAN      NOT NULL,
    spend_address   VARCHAR(255) NOT NULL,
    stealth_id      VARCHAR(255) NOT NULL,
    CONSTRAINT PK_OUTPUTS PRIMARY KEY (box_id, header_id)
);

CREATE INDEX "outputs__box_id" ON outputs (box_id);
CREATE INDEX "outputs__tx_id" ON outputs (tx_id);
CREATE INDEX "outputs__header_id" ON outputs (header_id);
CREATE INDEX "outputs__ergo_tree" ON outputs (ergo_tree);
CREATE INDEX "outputs__timestamp" ON outputs (timestamp);


CREATE TABLE outputs_fork
(
    box_id               VARCHAR(64)  NOT NULL,
    tx_id                VARCHAR(64)  NOT NULL,
    header_id            VARCHAR(64)  NOT NULL,
    value                BIGINT       NOT NULL,
    creation_height      INTEGER      NOT NULL,
    index                INTEGER      NOT NULL,
    ergo_tree            VARCHAR      NOT NULL,
    timestamp            BIGINT       NOT NULL,
    bytes                BLOB         NOT NULL,
    ADDITIONAL_REGISTERS VARCHAR(255) NOT NULL,
    ADDITIONAL_TOKENS    VARCHAR(255) NOT NULL,
    TX_INDEX             INTEGER      NOT NULL,
    SEQUENCE             BIGINT       NOT NULL AUTO_INCREMENT, ,
    CONSTRAINT PK_OUTPUTS_FORK PRIMARY KEY (box_id, header_id)
);

CREATE INDEX "outputs_fork_f__box_id" ON outputs_fork (box_id);
CREATE INDEX "outputs_fork_f__tx_id" ON outputs_fork (tx_id);
CREATE INDEX "outputs_fork_f__header_id" ON outputs_fork (header_id);
CREATE INDEX "outputs_fork_f__ergo_tree" ON outputs_fork (ergo_tree);
CREATE INDEX "outputs_fork_f__timestamp" ON outputs_fork (timestamp);

-- !Downs
