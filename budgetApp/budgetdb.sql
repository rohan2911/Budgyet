-- wip file
create table users {
    id integer not null auto_increment primary key,
    username varchar(26),
    email varchar(100) not nul,
    pw text,
    photo text,
    firstname varchar(100),
    lastname varchar(100),
    bio text
};

create table budgets {
    id integer not null auto_increment primary key,
    title varchar(100),
    "desc" varchar (1000),
    -- period  -- time period alloc'd? not sure
};

create table goals {
    id integer not null auto_increment primary key,
    title varchar(100),
    "desc" varchar(1000), 
    foreign key (budget) references budgets(id)
};

create table contact_lists {
    id integer not null auto_increment primary key,
    title varchar(100),
    foreign key (owner) references users(id)    
};

