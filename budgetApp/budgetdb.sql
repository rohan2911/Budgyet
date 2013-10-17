-- wip file
create table accounts (
    id bigint not null auto_increment,
    username varchar(30) not null,
    email varchar(100) not null,
    password varchar(30) not null,
    first_name varchar(100),
    last_name varchar(100),
    
    primary key (id)
);

-- create table budgets {
--    id integer not null auto_increment primary key,
--    title varchar(100),
--    "desc" varchar (1000),
--    -- period  -- time period alloc'd? not sure
-- };

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


create table incomes (
	id bigint not null auto_increment,
	owner bigint not null,
	amount decimal(20, 2),
    description text,
    income_date bigint,
	
	primary key (id),
	foreign key (owner) references accounts(id)
);

create table income_tags (
	id bigint not null auto_increment,
	owner bigint not null,
	name varchar(16),
	
	primary key (id),
	foreign key (owner) references accounts(id)
);

create table income_tags_map (
	income bigint not null,
	tag bigint not null,
	
	primary key (income, tag),
	foreign key (income) references incomes(id),
	foreign key (tag) references income_tags(id)
);

create table expenses (
    id bigint not null auto_increment,
    owner bigint not null,
    amount decimal(20, 2),
    description text,
    expense_date bigint,
    
    primary key (id),
	foreign key (owner) references accounts(id)
);

create table expense_tags (
	id bigint not null auto_increment,
	owner bigint,
	name varchar(16),
	
	primary key (id),
	foreign key (owner) references accounts(id)
);

create table expense_tags_map (
	expense bigint not null,
	tag bigint not null,

	primary key (expense, tag),
	foreign key (expense) references expenses(id),
	foreign key (tag) references expense_tags(id)
);