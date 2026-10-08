CREATE TABLE ml_model (
  id            uuid PRIMARY KEY,
  business_key  text NOT NULL UNIQUE,
  status        text NOT NULL,
  name          text NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ml_model_status_known
    CHECK (status IN ('DRAFT', 'EVALUATED', 'PRODUCTION', 'REJECTED'))
);