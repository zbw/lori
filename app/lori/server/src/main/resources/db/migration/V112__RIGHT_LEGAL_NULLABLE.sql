-- Remove not null constraint from has_legal_risk
ALTER TABLE item_right ALTER COLUMN has_legal_risk DROP NOT NULL;