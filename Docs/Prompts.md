# Prompts

## Prompt 1

> I'm starting this project of movie-ticketing platform. it is inside this repo. Please read assignment PDF and create only below files and structure for now.
>
> 1. AGENTS.MD - Use java 21, Spring boot, Maven, Postgres, Flyway, and other standard, keep the architecture monolith.
> 2. Prompts.MD - create a simple md file and record only all the prompts used during development.
> 3. README.md - add the project description, local setup, running tests, scope, assumptions and API documentation.
>
> Do not implement anything yet. Add just repo template code etc.

## Prompt 2

> Do not assume any design yet, You can remove design from readme file. We will iterate through step by step design iterations. Also put AGENTS.md , Prompts.md file in Docs folder.

## Prompt 3

> Let's design identity flow. create a design doc in docs folder under subfolder of designs and let's put scope,  HLD, LLD, schemas, API contracts, Diagram, etc. in that document.  Do not code it. I'll review it first.
>
> Idea is to create signing and signup flow for customers and admin users. For now we will go with JWT based simple authentication mechanism only. But we will write code in such a way that other mechanisms are easily pluggable.

## Prompt 4

> Let's make some modification in design.
>
> - let's have single user account table for both customer and admins, you misunderstood admin, here admin means theatre admins not the whole system admins we are taking about. For simplicity, keep the signup flow open for admin and use same table with roles, put separate table as out of scope for now.
> - store password hash in same table, no need for separate local creds table
> - create separate signup endpoint for customer and admin
> - remove invite flow for admin
> - use single singin endpoint, issued JWT token should contain account id and role details.
>
> update design doc for now.

## Prompt 5

> Design looks good, implement it. update all related documentations as well.
