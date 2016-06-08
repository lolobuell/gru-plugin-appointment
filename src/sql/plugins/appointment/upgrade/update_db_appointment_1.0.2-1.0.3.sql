INSERT INTO appointment_calendar_template (id, title, description, template_path) VALUES (4,'Calendrier jours ouverts','Calendrier des créneaux disponibles et indisponibles (jours ouverts)','skin/plugins/appointment/calendar/appointment_form_calendar_opendays.html' );
INSERT INTO appointment_calendar_template (id, title, description, template_path) VALUES (5,'Liste des créneaux disponibles jours ouverts','Liste des créneaux disponibles (jours ouverts)','skin/plugins/appointment/calendar/appointment_form_list_open_slots_opendays.html' );
ALTER TABLE appointment_form ADD COLUMN date_limit DATE NULL ;
ALTER TABLE appointment_form ADD COLUMN seizure_duration INT NOT NULL default '0';
ALTER TABLE appointment_form ADD COLUMN maximum_number_of_booked_seats INT NOT NULL default '0';
ALTER TABLE appointment_form ADD COLUMN address long varchar NULL;
ALTER TABLE appointment_form ADD COLUMN longitude float NULL;
ALTER TABLE appointment_form ADD COLUMN latitude float NULL;
ALTER TABLE appointment_form ADD COLUMN category varchar(255) NOT NULL default '';
