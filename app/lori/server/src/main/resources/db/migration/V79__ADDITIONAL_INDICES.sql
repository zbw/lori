CREATE INDEX restricted_open_content_licence_idx ON item_right (restricted_open_content_licence);
CREATE INDEX open_content_licence_lower_idx ON item_right (LOWER(open_content_licence));
CREATE INDEX open_content_licence_idx ON item_right (open_content_licence);
CREATE INDEX non_standard_open_content_licence_idx ON item_right (non_standard_open_content_licence);
CREATE INDEX zbw_user_agreement_idx ON item_right (zbw_user_agreement);
CREATE INDEX access_state_lower_idx ON item_right (LOWER(access_state));