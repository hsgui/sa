create table sentiment_words_with_trival(
 `id` int(11) NOT NULL AUTO_INCREMENT,
  `word` varchar(10) NOT NULL,
  `ranking` double(32,12) DEFAULT '0.000000000000',
  assess varchar(255) default '',
  `isSentiment` tinyint(4) DEFAULT '0',
  trival int(11) default 0,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM;

create table topic_statistics(
	id bigint not null auto_increment,
	topic_id int not null,
	start_time datetime not null,
	end_time datetime not null,
	positive_count int not null default 0,
	negative_count int not null default 0,
	primary key(id)
)

create table negative_status(
	id bigint(11) not null,
	topic_id int not null,
	generate_time datetime not null,
	flag tinyint(4) default 2
)

create table positive_status(
	id bigint(11) not null,
	topic_id int not null,
	generate_time datetime not null,
	flag tinyint(4) default 1
)

create table back_up_statuses(
	id bigint(11) not null auto_increment,
	weibo_mid varchar(255) not null,
	post_user varchar(255) not null,
	post_time datetime not null,
	related_topic_id int not null,
	content varchar(255) not null,
	primary key(id)
)

create table topics(
id int not null auto_increment,
name varchar(255) not null,
start_time datetime not null,
end_time datetime not null,
total_positive_count int default 0,
total_negative_count int default 0,
primary key(id),
unique key(name)
)