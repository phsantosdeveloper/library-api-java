INSERT INTO tb_author (name) VALUES ('Machado de Assis');
INSERT INTO tb_author (name) VALUES ('Jorge Amado');
INSERT INTO tb_author (name) VALUES ('George Orwell');
INSERT INTO tb_author (name) VALUES ('Jane Austen');
INSERT INTO tb_author (name) VALUES ('Mary Shelley');
INSERT INTO tb_author (name) VALUES ('Bram Stoker');

INSERT INTO tb_category (name) VALUES ('Romance');
INSERT INTO tb_category (name) VALUES ('Distopia');
INSERT INTO tb_category (name) VALUES ('Sátira');
INSERT INTO tb_category (name) VALUES ('Terror');

INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Dom Casmurro', '9788582850350', DATE '2016-07-26', 3, 1, 1);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Memórias póstumas de Brás Cubas', '9788582850015', DATE '2014-09-16', 2, 1, 1);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Capitães da areia', '9788535911695', DATE '2008-03-10', 4, 2, 1);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Gabriela, cravo e canela', '9788535912388', DATE '2008-07-03', 3, 2, 1);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Terras do sem-fim', '9788535912524', DATE '2008-07-03', 2, 2, 1);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('1984', '9788535932966', DATE '2019-12-06', 4, 3, 2);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('A revolução dos bichos', '9788535909555', DATE '2007-01-10', 3, 3, 3);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Orgulho e preconceito', '9788563560155', DATE '2011-06-21', 3, 4, 1);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Frankenstein', '9788571643987', DATE '1994-08-18', 2, 5, 4);
INSERT INTO tb_book (title, isbn, publication_date, total_copies, author_id, category_id) VALUES ('Drácula', '9788582850053', DATE '2014-10-07', 2, 6, 4);

INSERT INTO tb_user (name, email, phone, password) VALUES ('Ana Souza', 'ana.souza@example.com', '(11) 90000-0001', '12345678');
INSERT INTO tb_user (name, email, phone, password) VALUES ('Bruno Lima', 'bruno.lima@example.com', '(11) 90000-0002', '12345678');
INSERT INTO tb_user (name, email, phone, password) VALUES ('Carla Santos', 'carla.santos@example.com', '(11) 90000-0003', '12345678');
INSERT INTO tb_user (name, email, phone, password) VALUES ('Diego Alves', 'diego.alves@example.com', '(11) 90000-0004', '12345678');
INSERT INTO tb_user (name, email, phone, password) VALUES ('Elisa Rocha', 'elisa.rocha@example.com', '(11) 90000-0005', '12345678');

INSERT INTO tb_loan (user_id, book_id, loan_date, expected_return_date, status) VALUES (1, 1, DATE '2026-11-20', DATE '2026-12-20', 'ACTIVE');
INSERT INTO tb_loan (user_id, book_id, loan_date, expected_return_date, status) VALUES (2, 3, DATE '2026-11-20', DATE '2026-12-20', 'ACTIVE');
INSERT INTO tb_loan (user_id, book_id, loan_date, expected_return_date, status) VALUES (3, 6, DATE '2026-11-20', DATE '2026-12-20', 'ACTIVE');
INSERT INTO tb_loan (user_id, book_id, loan_date, expected_return_date, status) VALUES (4, 8, DATE '2026-11-20', DATE '2026-12-20', 'ACTIVE');
INSERT INTO tb_loan (user_id, book_id, loan_date, expected_return_date, status) VALUES (5, 10, DATE '2026-11-20', DATE '2026-12-20', 'ACTIVE');
