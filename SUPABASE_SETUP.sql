-- Boardscore collaborative rooms (review before running)
create table if not exists public.bs_rooms (
  token uuid primary key,
  state jsonb not null,
  revision integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint bs_state_size check (octet_length(state::text) <= 90000)
);
alter table public.bs_rooms enable row level security;
revoke all on public.bs_rooms from anon, authenticated, public;
create or replace function public.bs_create_room(p_token uuid,p_state jsonb)
returns void language plpgsql security definer set search_path = '' as $$
begin
 if octet_length(p_state::text)>90000 or not (p_state ? 'gameId') then raise exception 'invalid state'; end if;
 insert into public.bs_rooms(token,state) values(p_token,p_state);
end $$;
create or replace function public.bs_get_room(p_token uuid)
returns jsonb language sql stable security definer set search_path = '' as $$
 select jsonb_build_object('state',state,'revision',revision) from public.bs_rooms where token=p_token;
$$;
create or replace function public.bs_update_room(p_token uuid,p_state jsonb,p_revision integer)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare new_rev integer;
begin
 if octet_length(p_state::text)>90000 or not (p_state ? 'gameId') then raise exception 'invalid state'; end if;
 update public.bs_rooms set state=p_state,revision=revision+1,updated_at=now()
 where token=p_token and revision=p_revision returning revision into new_rev;
 if new_rev is null then return null; end if;
 return jsonb_build_object('revision',new_rev);
end $$;
revoke all on function public.bs_create_room(uuid,jsonb) from public;
revoke all on function public.bs_get_room(uuid) from public;
revoke all on function public.bs_update_room(uuid,jsonb,integer) from public;
grant execute on function public.bs_create_room(uuid,jsonb),public.bs_get_room(uuid),public.bs_update_room(uuid,jsonb,integer) to anon,authenticated;
