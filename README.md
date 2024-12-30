# Merge request assistant: creating pull request on GitHub

## How it works:
- **Settings specification**: you can specify base branch, head branch, name and content of the file you want to add, title and description of the PR. Or you can skip these steps (just press Enter) and go with the default settings.
- **Repository selection**: you can select a repository from your GitHub account
- **Branch creation**: the program creates a new branch with the specified or default name.
- **File management**: the program adds (or suggests to replace it, if it exists) the specified file in the selected branch.
- **Pull request creation**: the program creates a pull request with the specified or default title and description.
- **Error handling and interactivity**: in case of errors, the program gives informative feedback and suggests some options.

## Requirements:
- Kotlin 1.8+.
- GitHub Personal Access Token with sufficient permissions.
- Oracle JDK 17.
- Ktor (version 3.0.2 was used)
- config.json in src/main/resources with the following format:
{
  "githubToken": "your-github-token"
}
(example file is provided).

## Installation:
- Clone the repository:
git clone https://github.com/Allex-Nik/merge-request-assistant.git
- Build the project and run the application.

## Usage:
If you do not type anything, the default value will be used.
- Enter the name of the base branch (default: main).
- Enter the name of the head branch to create (default: hello).
- Enter the name of the file you want to add (default: Hello.txt).
- Provide the content for the file (default: Hello World).
- Provide a title for your pull request (default: Add Hello.txt).
- Provide a description for your pull request (default: Added Hello.txt with Hello World).

## Examples:
1. Ordinary use case:
```
Enter the name of the base branch. Default: main

Enter the name of the head branch. Default: hello

Enter the name of the file you want to add. Default: Hello.txt

Enter the content of the file you want to add. Default: Hello World

Enter the title of your pull request. Default: Add Hello.txt

Enter the description of your pull request. Default: Added Hello.txt with Hello world

Available repositories: 4
1. Name: test_repo, URL: [url], Private: true
2. Name: test_repo_2, URL: [url], Private: true
3. Name: test_repo_3, URL: [url], Private: true
4. Name: test_repo_4, URL: [url], Private: false
Enter the number of the repository: 
1
Selected repository: test_repo
Branch hello created successfully. HTTP status: 201 Created
File added successfully. HTTP status: 201 Created
Pull request created successfully. HTTP status: 201 Created
Response: {"url":[url],"id":[id],"node_id":[node_id],"html_url":[html_url],"diff_url":[diff_url], ...} (shortened and changed in the example)
All steps completed successfully.

Process finished with exit code 0
```

2. Branch already exists, use the same branch, replace the file:
```
Enter the name of the base branch. Default: main

Enter the name of the head branch. Default: hello

Enter the name of the file you want to add. Default: Hello.txt

Enter the content of the file you want to add. Default: Hello World

Enter the title of your pull request. Default: Add Hello.txt

Enter the description of your pull request. Default: Added Hello.txt with Hello world

Available repositories: 4
1. Name: test_repo, URL: [url], Private: true
2. Name: test_repo_2, URL: [url], Private: true
3. Name: test_repo_3, URL: [url], Private: true
4. Name: test_repo_4, URL: [url], Private: false
Enter the number of the repository: 
1
Selected repository: test_repo
Unprocessable entity. HTTP status: 422 Unprocessable Entity
Branch already exists
Do you want to use the existing branch? (yes/no)
yes
File Hello.txt already exists in branch hello.
Do you want to replace the file? (yes/no)
yes
File Hello.txt is updated successfully in branch hello
Pull request created successfully. HTTP status: 201 Created
Response: {"url":[url],"id":[id],"node_id":[node_id],"html_url":[html_url],"diff_url":[diff_url], ...} (shortened and changed in the example)
All steps completed successfully.

Process finished with exit code 0
```
