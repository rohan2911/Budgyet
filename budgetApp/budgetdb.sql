-- wip file
create table accounts (
    id bigint not null auto_increment,
    username varchar(30) not null unique,
    email varchar(100) not null unique,
    password varchar(30) not null,
    first_name varchar(100),
    last_name varchar(100),
    
    primary key (id)
);


-- create table goals {
--    id integer not null auto_increment primary key,
--    title varchar(100),
--    "desc" varchar(1000), 
--    foreign key (budget) references budgets(id)
-- };

-- create table contact_lists {
--    id integer not null auto_increment primary key,
--    title varchar(100),
--    foreign key (owner) references users(id)    
-- };



create table incomes_tags (
	id bigint not null auto_increment,
	owner bigint not null,
	name text,
	
	primary key (id),
	foreign key (owner) references accounts(id)
);

create table scheduled_incomes (
	id bigint not null auto_increment,
	owner bigint not null,
	date_next date,
	period int,
	income_amount decimal(16, 2),
    income_description varchar(256),
    tag bigint,

	primary key (id),
	foreign key (owner) references accounts(id),
	foreign key (tag) references incomes_tags(id)
);


create table incomes (
	id bigint not null auto_increment,
	owner bigint not null,
	scheduler bigint,
	amount decimal(16, 2),
    description varchar(256),
    date_occur date,
    tag bigint,
	
	primary key (id),
	foreign key (owner) references accounts(id),
	foreign key (scheduler) references scheduled_incomes(id),
	foreign key (tag) references incomes_tags(id)
);


-- create table incomes_tags_map (
-- 	income bigint not null,
-- 	tag bigint not null,
	
-- 	primary key (income, tag),
-- 	foreign key (income) references incomes(id),
-- 	foreign key (tag) references incomes_tags(id)
-- );

-- create table scheduled_incomes_tags_map (
-- 	scheduled_income bigint not null,
-- 	tag bigint not null,
	
-- 	primary key (scheduled_income, tag),
-- 	foreign key (scheduled_income) references scheduled_incomes(id),
-- 	foreign key (tag) references incomes_tags(id)
-- );





create table expenses_tags (
	id bigint not null auto_increment,
	owner bigint,
	name text,
	
	primary key (id),
	foreign key (owner) references accounts(id)
);

create table scheduled_expenses (
	id bigint not null auto_increment,
	owner bigint not null,
	date_next date,
	period int,
	expense_amount decimal(16, 2),
    expense_description varchar(256),
    tag bigint,

	primary key (id),
	foreign key (owner) references accounts(id), 
	foreign key (tag) references expenses_tags(id)
);


create table expenses (
    id bigint not null auto_increment,
    owner bigint not null,
    scheduler bigint,
    amount decimal(16, 2),
    description varchar(256),
    date_occur date,
    tag bigint,
    
    primary key (id),
	foreign key (owner) references accounts(id),
	foreign key (scheduler) references scheduled_expenses(id),
	foreign key (tag) references expenses_tags(id)
);


-- create table expenses_tags_map (
-- 	expense bigint not null,
-- 	tag bigint not null,

-- 	primary key (expense, tag),
-- 	foreign key (expense) references expenses(id),
-- 	foreign key (tag) references expenses_tags(id)
-- );

-- create table scheduled_expenses_tags_map (
-- 	scheduled_expense bigint not null,
-- 	tag bigint not null,

-- 	primary key (scheduled_expense, tag),
-- 	foreign key (scheduled_expense) references scheduled_expenses(id),
-- 	foreign key (tag) references expenses_tags(id)
-- );


create table budgets (
	id bigint not null auto_increment,
	owner bigint not null,
	title varchar(100),
	amount decimal(16, 2),
    description varchar(256),
    date_start date,
    date_end date,
	
	primary key (id),
	foreign key (owner) references accounts(id)
);

create table budgets_tags_map (
	budget bigint not null,
	tag bigint not null,

	primary key (budget, tag),
	foreign key (budget) references budgets(id),
	foreign key (tag) references expenses_tags(id)
);