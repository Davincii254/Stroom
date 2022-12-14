-- ------------------------------------------------------------------------
-- Copyright 2020 Crown Copyright
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- ------------------------------------------------------------------------

-- Stop NOTE level warnings about objects (not)? existing
SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;

-- Dropping the table and recreating is not really any different to
-- someone editing the retention rules as that would also cause the
-- content to be ignored. Simpler than writing a java migration for the
-- data.
DROP TABLE IF EXISTS `meta_retention_tracker`;

--
-- Create the meta_retention_tracker table
--
CREATE TABLE IF NOT EXISTS `meta_retention_tracker` (
    `retention_rules_version`  varchar(255) NOT NULL,
    `rule_age`                 varchar(255) NOT NULL,
    `last_run_time`            bigint NOT NULL,
    PRIMARY KEY                (retention_rules_version, rule_age)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set tabstop=4 shiftwidth=4 expandtab:
