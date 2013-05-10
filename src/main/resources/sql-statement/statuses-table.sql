create table statuses(
	status_id bigint not null,
	content varchar(500) not null,
	created_at timestamp not null,
	primary key(status_id)
)